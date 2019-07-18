package com.oo.resume.activity

import android.os.Bundle
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentManager
import androidx.fragment.app.FragmentPagerAdapter
import com.chenenyu.router.annotation.Route
import com.oo.platform.view.BaseActivity
import com.oo.resume.R
import kotlinx.android.synthetic.main.activity_home.*

/**
 *  $author yangchao
 *  $email  cd.uestc.superyoung@gmail.com
 *  $date   2019-06-13 17:21
 *  $describe
 */
@Route(RouteUrl.HOME_PAGE)
class HomeActivity : BaseActivity() {


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        initView()
    }

    private fun initView() {
        pager.adapter = HomeFragmetPageAdapter(supportFragmentManager)
        tabs.setupWithViewPager(pager)
        Page.values().forEachIndexed { index, page -> tabs.getTabAt(index)?.text = page.title }
    }

    inner class HomeFragmetPageAdapter(manager: FragmentManager) : FragmentPagerAdapter(manager,BEHAVIOR_RESUME_ONLY_CURRENT_FRAGMENT) {
        override fun getItem(position: Int): Fragment {
            when (Page.getPage(position)) {
                Page.ResumeList -> return ResumeListFragment()
                Page.Mine -> return MineFragment()
                else -> throw IllegalArgumentException("Did not handle this page enum!!!")
            }
        }

        override fun getCount(): Int {
            return Page.values().size
        }

    }

    enum class Page(val index: Int, val title: String) {
        ResumeList(0, "简历"),
        Mine(1, "我的");

        companion object {
            fun getPage(index: Int): Page? {
                return Page.values().find { it.index == index }
            }
        }
    }

    override fun getContentViewResId(): Int {
        return R.layout.activity_home
    }

}